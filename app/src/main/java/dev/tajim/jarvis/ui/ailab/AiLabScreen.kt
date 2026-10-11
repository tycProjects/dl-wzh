package dev.tajim.jarvis.ui.ailab

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.tajim.jarvis.ui.navigation.Workspace
import dev.tajim.jarvis.ui.settings.NavRow
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing
import dev.tajim.jarvis.ui.workspace.WorkspaceScaffold

/** AI Lab lists only tools that really exist. Image and sound generation are not offered because none is connected. */
@Composable
fun AiLabScreen(onClose: () -> Unit, onOpenChat: () -> Unit, onOpenAiSettings: () -> Unit) {
    val c = LocalJarvisColors.current
    WorkspaceScaffold(Workspace.AI_LAB, onClose) {
        NavRow("AI Chat Assistant", "Powered by your Gemini model", onOpenChat)
        NavRow("Code Assistant", "Chat with the Code switch on", onOpenChat)
        NavRow("Provider & model", "API key and model choice", onOpenAiSettings)
        Text(
            "More AI tools will appear here only when they are really connected.",
            modifier = Modifier.padding(top = Spacing.md), style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
        )
    }
}
