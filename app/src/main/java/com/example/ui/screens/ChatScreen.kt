package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.ui.theme.CoralError
import com.example.ui.theme.CosmicDeep
import com.example.ui.theme.CosmicGlassBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AssistantMode

@Composable
fun ChatScreen(
    sessions: List<ChatSessionEntity>,
    currentSessionId: Long?,
    messages: List<ChatMessageEntity>,
    assistantMode: AssistantMode,
    listState: LazyListState = rememberLazyListState(),
    onSelectSession: (Long) -> Unit,
    onNewChat: () -> Unit,
    onClearChat: () -> Unit,
    onSendMessage: (String) -> Unit,
    onVoiceInputClick: () -> Unit,
    onStopGeneration: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CosmicVoid, CosmicDeep)))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "মায়া চ্যাট",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Row {
                    IconButton(onClick = onNewChat, modifier = Modifier.testTag("chat_new_btn")) {
                        Icon(Icons.Rounded.AddComment, contentDescription = "নতুন চ্যাট", tint = NeonCyan)
                    }
                    IconButton(onClick = onClearChat, modifier = Modifier.testTag("chat_clear_btn")) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "চ্যাট পরিষ্কার", tint = CoralError)
                    }
                }
            }

            // Session History Chips
            if (sessions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        FilterChip(
                            selected = session.id == currentSessionId,
                            onClick = { onSelectSession(session.id) },
                            label = {
                                Text(
                                    text = session.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonIndigo.copy(alpha = 0.35f),
                                selectedLabelColor = NeonCyan,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                        ) {
                            Text(
                                text = "মায়াকে বাংলায় বা Banglish-এ যেকোনো প্রশ্ন করো—যেমন: \"আজ আবহাওয়া কেমন?\" অথবা \"ইউটিউব খোলো\"।",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Column(
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (isUser) 18.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 18.dp
                                    )
                                )
                                .background(
                                    if (isUser) Brush.linearGradient(listOf(NeonIndigo, NeonPurple))
                                    else Brush.verticalGradient(listOf(CosmicSurfaceElevated, CosmicSurface))
                                )
                                .border(1.dp, CosmicGlassBorder, RoundedCornerShape(18.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }

                        if (!isUser && msg.text.isNotBlank() && msg.text != "…") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 4.dp, start = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onSpeakMessage(msg.text) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "শোনো", tint = NeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("শোনো", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("MAYA", msg.text))
                                            Toast.makeText(context, "কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "কপি", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("কপি", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Input Row above Bottom Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 104.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("বাংলায় লিখো…", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (assistantMode == AssistantMode.THINKING || assistantMode == AssistantMode.SPEAKING) {
                                IconButton(
                                    onClick = onStopGeneration,
                                    modifier = Modifier.testTag("chat_stop_btn")
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = "থামাও", tint = CoralError)
                                }
                            } else {
                                IconButton(
                                    onClick = onVoiceInputClick,
                                    modifier = Modifier.testTag("chat_voice_btn")
                                ) {
                                    Icon(Icons.Rounded.Mic, contentDescription = "ভয়েস", tint = NeonPink)
                                }
                            }
                            if (inputText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onSendMessage(inputText)
                                        inputText = ""
                                    },
                                    modifier = Modifier.testTag("chat_send_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "পাঠাও", tint = NeonCyan)
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = CosmicGlassBorder,
                        focusedContainerColor = CosmicSurface,
                        unfocusedContainerColor = CosmicSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("chat_message_input")
                )
            }
        }
    }
}
