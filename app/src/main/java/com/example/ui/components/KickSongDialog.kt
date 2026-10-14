package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Song
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfWarningYellow

@Composable
fun KickSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: (notes: String?) -> Unit
) {
    val colors = JnmfTheme.colors
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JnmfSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.borderGlow, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = JnmfWarningYellow
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "KICK SONG FROM JNMF",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = JnmfWarningYellow
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = JnmfTextPrimary
                )
                Text(
                    text = "${song.artist} • ${song.formattedDuration}",
                    fontSize = 13.sp,
                    color = JnmfTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = JnmfDarkBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, JnmfBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "ℹ️ SAFE HIDE GUARANTEE:",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This song will be hidden from the JNMF player library only. The original music file will NOT be moved, altered, or deleted from your device.",
                            fontSize = 12.sp,
                            color = JnmfTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You can restore this song at any time from: Settings → Library Management → Kicked Songs.",
                            fontSize = 11.sp,
                            color = JnmfTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason / Notes (Optional)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Broken drop, low bitrate", fontSize = 12.sp, color = JnmfTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = JnmfBorder,
                        focusedTextColor = JnmfTextPrimary,
                        unfocusedTextColor = JnmfTextPrimary
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JnmfTextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onConfirm(notes.ifBlank { null }) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JnmfWarningYellow,
                            contentColor = JnmfDarkBg
                        )
                    ) {
                        Text("Kick Song", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
