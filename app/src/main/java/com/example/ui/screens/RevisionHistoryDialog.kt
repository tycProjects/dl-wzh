package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Note
import com.example.data.model.NoteRevision
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils

@Composable
fun RevisionHistoryDialog(
    isOpen: Boolean,
    note: Note?,
    revisions: List<NoteRevision>,
    onDismiss: () -> Unit,
    onRestoreRevision: (NoteRevision) -> Unit
) {
    if (!isOpen || note == null) return

    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var selectedRevision by remember { mutableStateOf<NoteRevision?>(revisions.firstOrNull()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = colors.panelBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.historyRevisions,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (revisions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded revisions yet for this note.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                        )
                    }
                } else {
                    Row(modifier = Modifier.weight(1f)) {
                        // Revisions List Column (Left)
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 6.dp)
                        ) {
                            items(revisions, key = { it.id }) { rev ->
                                val isSelected = selectedRevision?.id == rev.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedRevision = rev },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.cardBackground
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isSelected) colors.accent else colors.border
                                        )
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = TextFormatUtils.formatDate(rev.createdAt),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colors.accent else colors.textPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = rev.revisionLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = colors.textSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = "${TextFormatUtils.getWordCount(rev.content)} words",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = colors.textSecondary.copy(alpha = 0.7f),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Diff / Preview Column (Right)
                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .background(colors.cardBackground, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            val activeRev = selectedRevision
                            if (activeRev != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.compareWithCurrent,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textSecondary
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            onRestoreRevision(activeRev)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accent,
                                            contentColor = colors.background
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.restoreRevision, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val diffLines = remember(activeRev, note.content) {
                                    TextFormatUtils.computeSimpleDiff(activeRev.content, note.content)
                                }

                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(diffLines) { diff ->
                                        val (bgColor, textColor, prefix) = when (diff.type) {
                                            TextFormatUtils.DiffType.ADDED -> Triple(
                                                Color(0xFF1E3A24),
                                                Color(0xFF81C784),
                                                "+ "
                                            )
                                            TextFormatUtils.DiffType.REMOVED -> Triple(
                                                Color(0xFF3E1F1F),
                                                Color(0xFFE57373),
                                                "- "
                                            )
                                            TextFormatUtils.DiffType.SAME -> Triple(
                                                Color.Transparent,
                                                colors.textSecondary,
                                                "  "
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(bgColor)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$prefix${diff.text}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = textColor
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
