package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.crypto.SecretFormat
import com.example.data.AppDatabase
import com.example.data.MessageRecord
import com.example.localization.LanguageManager
import com.example.ui.components.GlassCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import kotlinx.coroutines.launch

@Composable
fun DecodeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var inputEncoded by remember { mutableStateOf("") }
    var detectedFormat by remember { mutableStateOf<SecretFormat?>(null) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var decodedResult by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun detectAndUpdate(text: String) {
        inputEncoded = text
        detectedFormat = CryptoEngine.detectFormat(text)
        errorMessage = null
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            detectAndUpdate(text)
            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    fun executeDecoding() {
        errorMessage = null
        val text = inputEncoded.trim()
        if (text.isEmpty()) {
            errorMessage = "Please paste or enter an encoded message"
            return
        }

        val format = detectedFormat ?: CryptoEngine.detectFormat(text) ?: SecretFormat.EMOJI

        try {
            val result = when (format) {
                SecretFormat.STEGANOGRAPHY -> CryptoEngine.decodeSteganography(text)
                SecretFormat.EMOJI -> CryptoEngine.decodeEmoji(text)
                SecretFormat.CHARACTER -> CryptoEngine.decodeCharacter(text)
                SecretFormat.AES256_GCM -> {
                    if (password.isEmpty()) {
                        errorMessage = LanguageManager.get("err_wrong_password") + " (Password is required)"
                        return
                    }
                    CryptoEngine.decryptAes256Gcm(text, password)
                }
            }

            if (result.isSuccess) {
                val decodedText = result.getOrNull() ?: ""
                decodedResult = decodedText

                // Save to room history asynchronously
                coroutineScope.launch {
                    val db = AppDatabase.getInstance(context)
                    db.messageDao().insertMessage(
                        MessageRecord(
                            type = "DECODED",
                            format = format.id,
                            originalText = text,
                            resultText = decodedText,
                            isProtected = format == SecretFormat.AES256_GCM
                        )
                    )
                }
                Toast.makeText(context, LanguageManager.get("saved_to_history"), Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: LanguageManager.get("err_decode_failed")
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: LanguageManager.get("err_decode_failed")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = LanguageManager.get("decode_title"),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = LanguageManager.get("decode_subtitle"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        // 1-Tap Paste Button
        Button(
            onClick = { pasteFromClipboard() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_quick_paste_clipboard"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = LanguageManager.get("btn_paste_from_clipboard"),
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Input Card
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.get("label_paste_message"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (inputEncoded.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                inputEncoded = ""
                                detectedFormat = null
                                decodedResult = ""
                                errorMessage = null
                            },
                            modifier = Modifier.size(32.dp).testTag("decode_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = inputEncoded,
                    onValueChange = { detectAndUpdate(it) },
                    placeholder = {
                        Text(
                            text = "Paste message (e.g. 🔐[SC-EMOJI]... or 🛡️[SC-AES256]...)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("decode_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                // Format detection pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = LanguageManager.get("detected_format") + ":",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (detectedFormat != null) NeonEmerald.copy(alpha = 0.2f)
                                    else Color(0xFF1E293B)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = detectedFormat?.let {
                                    "${it.icon} ${if (LanguageManager.isBengali()) it.displayNameBn else it.displayNameEn}"
                                } ?: LanguageManager.get("format_unknown"),
                                color = if (detectedFormat != null) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "${inputEncoded.length} chars",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Password Prompt if AES-256 detected or selected
        AnimatedVisibility(visible = detectedFormat == SecretFormat.AES256_GCM) {
            GlassCard(
                borderBrush = androidx.compose.ui.graphics.SolidColor(NeonAmber.copy(alpha = 0.5f))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = LanguageManager.get("label_decrypt_password"),
                            color = NeonAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(LanguageManager.get("hint_decrypt_password"), fontSize = 12.sp) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("decode_password_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NeonRose.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⚠️ $errorMessage",
                    color = NeonRose,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Execute Decode Button
        Button(
            onClick = { executeDecoding() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_decode_message"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text(
                text = LanguageManager.get("btn_decode"),
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Decoded Result Card
        AnimatedVisibility(visible = decodedResult.isNotEmpty()) {
            GlassCard(
                borderBrush = androidx.compose.ui.graphics.SolidColor(NeonEmerald.copy(alpha = 0.5f))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.get("label_decoded_result"),
                            color = NeonEmerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Decoded Message", decodedResult)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, LanguageManager.get("msg_decoded_copied"), Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.2f)),
                                modifier = Modifier.height(34.dp).testTag("copy_decoded_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = LanguageManager.get("btn_copy"),
                                    color = NeonEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, decodedResult)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share original message via..."))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.2f)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = decodedResult,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
