package dev.tajim.jarvis.ui.workspace

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.HoloButton
import dev.tajim.jarvis.ui.navigation.Workspace
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing

@Composable
fun TextLabScreen(viewModel: TextLabViewModel, onClose: () -> Unit) {
    val c = LocalJarvisColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val text = viewModel.text
    val copiedMessage = stringResource(R.string.tl_copied)
    var confirmClear by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { viewModel.flush() } }

    WorkspaceScaffold(Workspace.TEXT_LAB, onClose) {
        if (text == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@WorkspaceScaffold
        }
        val words = remember(text) { text.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
        val lines = remember(text) { if (text.isEmpty()) 0 else text.count { it == '\n' } + 1 }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatCard(stringResource(R.string.tl_words), words, Modifier.weight(1f))
            StatCard(stringResource(R.string.tl_chars), text.length, Modifier.weight(1f))
            StatCard(stringResource(R.string.tl_lines), lines, Modifier.weight(1f))
        }
        if (viewModel.locked) {
            Text(
                stringResource(R.string.tl_locked),
                modifier = Modifier.padding(top = Spacing.sm),
                style = MaterialTheme.typography.labelMedium, color = c.warning,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = viewModel::onTextChange,
            readOnly = viewModel.locked,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = Spacing.md),
            placeholder = { Text(stringResource(R.string.tl_hint)) },
        )
        Row(
            Modifier.fillMaxWidth().padding(bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            HoloButton(
                stringResource(if (viewModel.locked) R.string.tl_unlock else R.string.tl_lock),
                onClick = viewModel::toggleLock, modifier = Modifier.weight(1f),
            )
            HoloButton(
                stringResource(R.string.tl_copy),
                onClick = {
                    clipboard.setText(AnnotatedString(text))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f), enabled = text.isNotEmpty(),
            )
            HoloButton(
                stringResource(R.string.tl_clear),
                onClick = { confirmClear = true },
                modifier = Modifier.weight(1f), enabled = text.isNotEmpty() && !viewModel.locked,
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.tl_clear_title)) },
            text = { Text(stringResource(R.string.tl_clear_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clear(); confirmClear = false }) { Text(stringResource(R.string.tl_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier) {
    val c = LocalJarvisColors.current
    GlassCard(modifier) {
        Column(Modifier.padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
    }
}
