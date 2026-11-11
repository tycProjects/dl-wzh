package com.logalyze.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.logalyze.app.LocalSettings
import com.logalyze.app.R
import com.logalyze.app.chat.AiConfig
import com.logalyze.app.chat.ChatMessage
import com.logalyze.app.ui.theme.ErrorRed

private fun buildSystemPrompt(r: AnalysisResult, lang: String): String = buildString {
    append("You are Logalyze, a friendly Minecraft troubleshooting assistant. ")
    append("Give short, practical, step-by-step help. Do not invent log lines or mod names. ")
    append(if (lang == "ar") "Reply in simple Arabic. " else "Reply in English. ")
    append("\n\nThe user's latest log analysis:\n")
    append("Error: ${r.errorType}\nSummary: ${r.summary}\nRoot cause: ${r.rootCause}\n")
    if (r.evidence.isNotEmpty()) append("Evidence: ${r.evidence.joinToString(" | ")}\n")
    append("Suggested fixes: ${r.fixes.joinToString("; ")}\n")
    r.launcher?.let { append("Launcher: $it\n") }
    if (r.deviceInfo.isNotEmpty()) {
        append("Device: ${r.deviceInfo.joinToString("; ") { "${it.first}=${it.second}" }}\n")
    }
    if (r.mods.isNotEmpty()) append("Suspected mods: ${r.mods.joinToString(", ")}\n")
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            msg.text,
            color = if (msg.fromUser) scheme.onPrimary else scheme.onBackground,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (msg.fromUser) scheme.primary else scheme.surfaceVariant)
                .padding(12.dp)
        )
    }
}

@Composable
fun ChatScreen(item: HistoryItem, onBack: () -> Unit) {
    val settings = LocalSettings.current
    val vm: ChatViewModel = viewModel()
    val system = remember(item.id, settings.language) { buildSystemPrompt(item.result, settings.language) }
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val onBg = MaterialTheme.colorScheme.onBackground
    val suggest1 = stringResource(R.string.chat_suggest_1)
    val suggest2 = stringResource(R.string.chat_suggest_2)

    LaunchedEffect(item.id) { vm.start(item.id) }
    LaunchedEffect(vm.messages.size, vm.sending) {
        val total = vm.messages.size + if (vm.sending) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    fun send(text: String) {
        vm.send(text, system)
        input = ""
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.chat_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onBg)
        }

        if (!AiConfig.isConfigured) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.chat_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (vm.messages.isEmpty() && !vm.sending) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Pill(suggest1, false) { send(suggest1) }
                            Pill(suggest2, false) { send(suggest2) }
                        }
                    }
                }
                items(vm.messages) { msg -> Bubble(msg) }
                if (vm.sending) {
                    item { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp) }
                }
                vm.error?.let { err ->
                    item {
                        Text(
                            "${stringResource(R.string.chat_error)}: $err",
                            color = ErrorRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.chat_hint)) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = onBg,
                        unfocusedTextColor = onBg,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { send(input) },
                    enabled = input.isNotBlank() && !vm.sending
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send),
                        tint = if (input.isNotBlank() && !vm.sending) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
