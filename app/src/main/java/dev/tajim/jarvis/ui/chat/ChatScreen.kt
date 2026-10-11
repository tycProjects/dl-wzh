package dev.tajim.jarvis.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tajim.jarvis.data.ChatMessage
import dev.tajim.jarvis.data.Role
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.HoloIconButton
import dev.tajim.jarvis.ui.components.JarvisBackground
import dev.tajim.jarvis.ui.components.tabContentInsets
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

@Composable
fun ChatScreen(viewModel: ChatViewModel, onMicClick: () -> Unit, onOpenAiSettings: () -> Unit) {
    val c = LocalJarvisColors.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, viewModel.sending) {
        val extra = if (viewModel.sending) 1 else 0
        if (messages.size + extra > 0) listState.animateScrollToItem(messages.size + extra - 1)
    }

    JarvisBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(tabContentInsets())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp))
                Text(
                    "Jarvis", modifier = Modifier.weight(1f),
                    style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp, brush = Brush.linearGradient(listOf(c.cyan, c.blue, c.violet))),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                HoloIconButton(Icons.Outlined.AddComment, "New conversation", viewModel::newChat)
            }

            if (!settings.hasApiKey || settings.aiModel.isBlank()) {
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs), onClick = onOpenAiSettings) {
                    Text(
                        if (!settings.hasApiKey) "No AI key yet. Tap to add your Gemini API key." else "No model selected. Tap to choose one.",
                        modifier = Modifier.padding(Spacing.md), style = MaterialTheme.typography.bodyMedium, color = c.warning,
                    )
                }
            }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (messages.isEmpty()) {
                    item { Text("Say something to JARVIS. Voice conversations appear here too.", color = c.textSecondary, style = MaterialTheme.typography.bodyMedium) }
                }
                items(messages, key = { it.id }) { m -> Bubble(m, onRetry = { viewModel.retry(m.id) }) }
                if (viewModel.sending) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Thinking…", color = c.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            viewModel.error?.let { err ->
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs), onClick = viewModel::dismissError) {
                    Text(err, modifier = Modifier.padding(Spacing.md), style = MaterialTheme.typography.bodyMedium, color = c.error)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message Jarvis...") },
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 4,
                    trailingIcon = {
                        if (viewModel.sending) {
                            IconButton(onClick = viewModel::stop) { Icon(Icons.Outlined.Stop, "Stop generating", tint = c.error) }
                        } else {
                            IconButton(onClick = { viewModel.send(input); input = "" }, enabled = input.isNotBlank()) { Icon(Icons.Outlined.Send, "Send", tint = c.cyan) }
                        }
                    },
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(selected = !viewModel.codeMode, onClick = { viewModel.codeMode = false }, label = { Text("Jarvis") })
                FilterChip(selected = viewModel.codeMode, onClick = { viewModel.codeMode = true }, label = { Text("Code") })
                Box(Modifier.weight(1f))
                HoloIconButton(Icons.Outlined.Mic, "Talk to Jarvis", onMicClick)
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, onRetry: () -> Unit) {
    val c = LocalJarvisColors.current
    val mine = m.role == Role.USER
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .background(if (mine) Brush.linearGradient(listOf(c.blue.copy(alpha = 0.85f), c.violet.copy(alpha = 0.75f))) else Brush.linearGradient(listOf(c.glassFill, c.glassFill)), shape)
                .border(1.dp, Brush.linearGradient(listOf(c.borderStart, c.borderEnd)), shape)
                .padding(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                splitCode(m.text).forEach { part ->
                    if (part.code) CodeBlock(part.text) else Text(part.text.trim(), color = if (mine) androidx.compose.ui.graphics.Color.White else c.textPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(m.time)), fontSize = 10.sp, color = c.textSecondary)
            if (m.failed) {
                Text("  Failed to send", fontSize = 11.sp, color = c.error)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

private data class Part(val text: String, val code: Boolean)

/** Splits ``` fenced blocks from normal text. This is the only Markdown rendered; other Markdown shows as plain text. */
private fun splitCode(text: String): List<Part> {
    val pieces = text.split("```")
    return pieces.mapIndexedNotNull { i, p ->
        if (p.isBlank()) null
        else if (i % 2 == 1) Part(p.substringAfter('\n', p).trimEnd(), true) else Part(p, false)
    }.ifEmpty { listOf(Part(text, false)) }
}

@Composable
private fun CodeBlock(code: String) {
    val c = LocalJarvisColors.current
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0x33000000), RoundedCornerShape(12.dp)).padding(Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.ContentCopy, "Copy code", tint = c.cyan, modifier = Modifier.size(18.dp))
            }
        }
        Text(code, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = c.textPrimary)
    }
}
